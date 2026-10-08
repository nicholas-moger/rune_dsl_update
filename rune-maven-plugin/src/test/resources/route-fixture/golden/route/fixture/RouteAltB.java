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
import route.fixture.meta.RouteAltBMeta;

import static java.util.Optional.ofNullable;

/**
 * @version 1.0.0
 */
@RosettaDataType(value="RouteAltB", builder=RouteAltB.RouteAltBBuilderImpl.class, version="1.0.0")
@RuneDataType(value="RouteAltB", model="route", builder=RouteAltB.RouteAltBBuilderImpl.class, version="1.0.0")
public interface RouteAltB extends RosettaModelObject {

	RouteAltBMeta metaData = new RouteAltBMeta();

	/*********************** Getter Methods  ***********************/
	RouteLeaf getShared();
	String getQ();

	/*********************** Build Methods  ***********************/
	RouteAltB build();
	
	RouteAltB.RouteAltBBuilder toBuilder();
	
	static RouteAltB.RouteAltBBuilder builder() {
		return new RouteAltB.RouteAltBBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends RouteAltB> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends RouteAltB> getType() {
		return RouteAltB.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processRosetta(path.newSubPath("shared"), processor, RouteLeaf.class, getShared());
		processor.processBasic(path.newSubPath("q"), String.class, getQ(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface RouteAltBBuilder extends RouteAltB, RosettaModelObjectBuilder {
		RouteLeaf.RouteLeafBuilder getOrCreateShared();
		@Override
		RouteLeaf.RouteLeafBuilder getShared();
		RouteAltB.RouteAltBBuilder setShared(RouteLeaf shared);
		RouteAltB.RouteAltBBuilder setQ(String q);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processRosetta(path.newSubPath("shared"), processor, RouteLeaf.RouteLeafBuilder.class, getShared());
			processor.processBasic(path.newSubPath("q"), String.class, getQ(), this);
		}
		

		RouteAltB.RouteAltBBuilder prune();
	}

	/*********************** Immutable Implementation of RouteAltB  ***********************/
	class RouteAltBImpl implements RouteAltB {
		private final RouteLeaf shared;
		private final String q;
		
		protected RouteAltBImpl(RouteAltB.RouteAltBBuilder builder) {
			this.shared = ofNullable(builder.getShared()).map(f->f.build()).orElse(null);
			this.q = builder.getQ();
		}
		
		@Override
		@RosettaAttribute("shared")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("shared")
		public RouteLeaf getShared() {
			return shared;
		}
		
		@Override
		@RosettaAttribute("q")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("q")
		public String getQ() {
			return q;
		}
		
		@Override
		public RouteAltB build() {
			return this;
		}
		
		@Override
		public RouteAltB.RouteAltBBuilder toBuilder() {
			RouteAltB.RouteAltBBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(RouteAltB.RouteAltBBuilder builder) {
			ofNullable(getShared()).ifPresent(builder::setShared);
			ofNullable(getQ()).ifPresent(builder::setQ);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			RouteAltB _that = getType().cast(o);
		
			if (!Objects.equals(shared, _that.getShared())) return false;
			if (!Objects.equals(q, _that.getQ())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (shared != null ? shared.hashCode() : 0);
			_result = 31 * _result + (q != null ? q.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "RouteAltB {" +
				"shared=" + this.shared + ", " +
				"q=" + this.q +
			'}';
		}
	}

	/*********************** Builder Implementation of RouteAltB  ***********************/
	class RouteAltBBuilderImpl implements RouteAltB.RouteAltBBuilder {
	
		protected RouteLeaf.RouteLeafBuilder shared;
		protected String q;
		
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
		@RosettaAttribute("q")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("q")
		public String getQ() {
			return q;
		}
		
		@RosettaAttribute("shared")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("shared")
		@Override
		public RouteAltB.RouteAltBBuilder setShared(RouteLeaf _shared) {
			this.shared = _shared == null ? null : _shared.toBuilder();
			return this;
		}
		
		@RosettaAttribute("q")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("q")
		@Override
		public RouteAltB.RouteAltBBuilder setQ(String _q) {
			this.q = _q == null ? null : _q;
			return this;
		}
		
		@Override
		public RouteAltB build() {
			return new RouteAltB.RouteAltBImpl(this);
		}
		
		@Override
		public RouteAltB.RouteAltBBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public RouteAltB.RouteAltBBuilder prune() {
			if (shared!=null && !shared.prune().hasData()) shared = null;
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getShared()!=null && getShared().hasData()) return true;
			if (getQ()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public RouteAltB.RouteAltBBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			RouteAltB.RouteAltBBuilder o = (RouteAltB.RouteAltBBuilder) other;
			
			merger.mergeRosetta(getShared(), o.getShared(), this::setShared);
			
			merger.mergeBasic(getQ(), o.getQ(), this::setQ);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			RouteAltB _that = getType().cast(o);
		
			if (!Objects.equals(shared, _that.getShared())) return false;
			if (!Objects.equals(q, _that.getQ())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (shared != null ? shared.hashCode() : 0);
			_result = 31 * _result + (q != null ? q.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "RouteAltBBuilder {" +
				"shared=" + this.shared + ", " +
				"q=" + this.q +
			'}';
		}
	}
}
