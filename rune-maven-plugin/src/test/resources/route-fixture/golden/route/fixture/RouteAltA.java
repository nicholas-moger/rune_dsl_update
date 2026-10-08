package route.fixture;

import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.Accessor;
import com.rosetta.model.lib.annotations.AccessorType;
import com.rosetta.model.lib.annotations.RosettaAttribute;
import com.rosetta.model.lib.annotations.RosettaDataType;
import com.rosetta.model.lib.annotations.RuneAttribute;
import com.rosetta.model.lib.annotations.RuneDataType;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.BuilderMerger;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;
import java.util.Objects;
import route.fixture.meta.RouteAltAMeta;

import static java.util.Optional.ofNullable;

/**
 * eligible, zero deep features - the EMPTY DeepPathUtil class.
 * @version 1.0.0
 */
@RosettaDataType(value="RouteAltA", builder=RouteAltA.RouteAltABuilderImpl.class, version="1.0.0")
@RuneDataType(value="RouteAltA", model="route", builder=RouteAltA.RouteAltABuilderImpl.class, version="1.0.0")
public interface RouteAltA extends RosettaModelObject {

	RouteAltAMeta metaData = new RouteAltAMeta();

	/*********************** Getter Methods  ***********************/
	RouteLeaf getShared();
	String getP();

	/*********************** Build Methods  ***********************/
	RouteAltA build();
	
	RouteAltA.RouteAltABuilder toBuilder();
	
	static RouteAltA.RouteAltABuilder builder() {
		return new RouteAltA.RouteAltABuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends RouteAltA> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends RouteAltA> getType() {
		return RouteAltA.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processRosetta(path.newSubPath("shared"), processor, RouteLeaf.class, getShared());
		processor.processBasic(path.newSubPath("p"), String.class, getP(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface RouteAltABuilder extends RouteAltA, RosettaModelObjectBuilder {
		RouteLeaf.RouteLeafBuilder getOrCreateShared();
		@Override
		RouteLeaf.RouteLeafBuilder getShared();
		RouteAltA.RouteAltABuilder setShared(RouteLeaf shared);
		RouteAltA.RouteAltABuilder setP(String p);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processRosetta(path.newSubPath("shared"), processor, RouteLeaf.RouteLeafBuilder.class, getShared());
			processor.processBasic(path.newSubPath("p"), String.class, getP(), this);
		}
		

		RouteAltA.RouteAltABuilder prune();
	}

	/*********************** Immutable Implementation of RouteAltA  ***********************/
	class RouteAltAImpl implements RouteAltA {
		private final RouteLeaf shared;
		private final String p;
		
		protected RouteAltAImpl(RouteAltA.RouteAltABuilder builder) {
			this.shared = ofNullable(builder.getShared()).map(f->f.build()).orElse(null);
			this.p = builder.getP();
		}
		
		@Override
		@RosettaAttribute("shared")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("shared")
		public RouteLeaf getShared() {
			return shared;
		}
		
		@Override
		@RosettaAttribute("p")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("p")
		public String getP() {
			return p;
		}
		
		@Override
		public RouteAltA build() {
			return this;
		}
		
		@Override
		public RouteAltA.RouteAltABuilder toBuilder() {
			RouteAltA.RouteAltABuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(RouteAltA.RouteAltABuilder builder) {
			ofNullable(getShared()).ifPresent(builder::setShared);
			ofNullable(getP()).ifPresent(builder::setP);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			RouteAltA _that = getType().cast(o);
		
			if (!Objects.equals(shared, _that.getShared())) return false;
			if (!Objects.equals(p, _that.getP())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (shared != null ? shared.hashCode() : 0);
			_result = 31 * _result + (p != null ? p.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "RouteAltA {" +
				"shared=" + this.shared + ", " +
				"p=" + this.p +
			'}';
		}
	}

	/*********************** Builder Implementation of RouteAltA  ***********************/
	class RouteAltABuilderImpl implements RouteAltA.RouteAltABuilder {
	
		protected RouteLeaf.RouteLeafBuilder shared;
		protected String p;
		
		@Override
		@RosettaAttribute("shared")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("shared")
		public RouteLeaf.RouteLeafBuilder getShared() {
			return shared;
		}
		
		@Override
		public RouteLeaf.RouteLeafBuilder getOrCreateShared() {
			RouteLeaf.RouteLeafBuilder result;
			if (shared!=null) {
				result = shared;
			}
			else {
				result = shared = RouteLeaf.builder();
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("p")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("p")
		public String getP() {
			return p;
		}
		
		@RosettaAttribute("shared")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("shared")
		@Override
		public RouteAltA.RouteAltABuilder setShared(RouteLeaf _shared) {
			this.shared = _shared == null ? null : _shared.toBuilder();
			return this;
		}
		
		@RosettaAttribute("p")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("p")
		@Override
		public RouteAltA.RouteAltABuilder setP(String _p) {
			this.p = _p == null ? null : _p;
			return this;
		}
		
		@Override
		public RouteAltA build() {
			return new RouteAltA.RouteAltAImpl(this);
		}
		
		@Override
		public RouteAltA.RouteAltABuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public RouteAltA.RouteAltABuilder prune() {
			if (shared!=null && !shared.prune().hasData()) shared = null;
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getShared()!=null && getShared().hasData()) return true;
			if (getP()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public RouteAltA.RouteAltABuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			RouteAltA.RouteAltABuilder o = (RouteAltA.RouteAltABuilder) other;
			
			merger.mergeRosetta(getShared(), o.getShared(), this::setShared);
			
			merger.mergeBasic(getP(), o.getP(), this::setP);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			RouteAltA _that = getType().cast(o);
		
			if (!Objects.equals(shared, _that.getShared())) return false;
			if (!Objects.equals(p, _that.getP())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (shared != null ? shared.hashCode() : 0);
			_result = 31 * _result + (p != null ? p.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "RouteAltABuilder {" +
				"shared=" + this.shared + ", " +
				"p=" + this.p +
			'}';
		}
	}
}
