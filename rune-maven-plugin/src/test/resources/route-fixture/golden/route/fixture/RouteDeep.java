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
import route.fixture.meta.RouteDeepMeta;

import static java.util.Optional.ofNullable;

/**
 * DEEP-PATH ELIGIBLE - a one-of over two alternatives whose targets share `shared`.
 * @version 1.0.0
 */
@RosettaDataType(value="RouteDeep", builder=RouteDeep.RouteDeepBuilderImpl.class, version="1.0.0")
@RuneDataType(value="RouteDeep", model="route", builder=RouteDeep.RouteDeepBuilderImpl.class, version="1.0.0")
public interface RouteDeep extends RosettaModelObject {

	RouteDeepMeta metaData = new RouteDeepMeta();

	/*********************** Getter Methods  ***********************/
	RouteAltA getA();
	RouteAltB getB();

	/*********************** Build Methods  ***********************/
	RouteDeep build();
	
	RouteDeep.RouteDeepBuilder toBuilder();
	
	static RouteDeep.RouteDeepBuilder builder() {
		return new RouteDeep.RouteDeepBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends RouteDeep> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends RouteDeep> getType() {
		return RouteDeep.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processRosetta(path.newSubPath("a"), processor, RouteAltA.class, getA());
		processRosetta(path.newSubPath("b"), processor, RouteAltB.class, getB());
	}
	

	/*********************** Builder Interface  ***********************/
	interface RouteDeepBuilder extends RouteDeep, RosettaModelObjectBuilder {
		RouteAltA.RouteAltABuilder getOrCreateA();
		@Override
		RouteAltA.RouteAltABuilder getA();
		RouteAltB.RouteAltBBuilder getOrCreateB();
		@Override
		RouteAltB.RouteAltBBuilder getB();
		RouteDeep.RouteDeepBuilder setA(RouteAltA a);
		RouteDeep.RouteDeepBuilder setB(RouteAltB b);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processRosetta(path.newSubPath("a"), processor, RouteAltA.RouteAltABuilder.class, getA());
			processRosetta(path.newSubPath("b"), processor, RouteAltB.RouteAltBBuilder.class, getB());
		}
		

		RouteDeep.RouteDeepBuilder prune();
	}

	/*********************** Immutable Implementation of RouteDeep  ***********************/
	class RouteDeepImpl implements RouteDeep {
		private final RouteAltA a;
		private final RouteAltB b;
		
		protected RouteDeepImpl(RouteDeep.RouteDeepBuilder builder) {
			this.a = ofNullable(builder.getA()).map(f->f.build()).orElse(null);
			this.b = ofNullable(builder.getB()).map(f->f.build()).orElse(null);
		}
		
		@Override
		@RosettaAttribute("a")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("a")
		public RouteAltA getA() {
			return a;
		}
		
		@Override
		@RosettaAttribute("b")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("b")
		public RouteAltB getB() {
			return b;
		}
		
		@Override
		public RouteDeep build() {
			return this;
		}
		
		@Override
		public RouteDeep.RouteDeepBuilder toBuilder() {
			RouteDeep.RouteDeepBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(RouteDeep.RouteDeepBuilder builder) {
			ofNullable(getA()).ifPresent(builder::setA);
			ofNullable(getB()).ifPresent(builder::setB);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			RouteDeep _that = getType().cast(o);
		
			if (!Objects.equals(a, _that.getA())) return false;
			if (!Objects.equals(b, _that.getB())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (a != null ? a.hashCode() : 0);
			_result = 31 * _result + (b != null ? b.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "RouteDeep {" +
				"a=" + this.a + ", " +
				"b=" + this.b +
			'}';
		}
	}

	/*********************** Builder Implementation of RouteDeep  ***********************/
	class RouteDeepBuilderImpl implements RouteDeep.RouteDeepBuilder {
	
		protected RouteAltA.RouteAltABuilder a;
		protected RouteAltB.RouteAltBBuilder b;
		
		@Override
		@RosettaAttribute("a")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("a")
		public RouteAltA.RouteAltABuilder getA() {
			return a;
		}
		
		@Override
		public RouteAltA.RouteAltABuilder getOrCreateA() {
			RouteAltA.RouteAltABuilder result;
			if (a!=null) {
				result = a;
			}
			else {
				result = a = RouteAltA.builder();
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("b")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("b")
		public RouteAltB.RouteAltBBuilder getB() {
			return b;
		}
		
		@Override
		public RouteAltB.RouteAltBBuilder getOrCreateB() {
			RouteAltB.RouteAltBBuilder result;
			if (b!=null) {
				result = b;
			}
			else {
				result = b = RouteAltB.builder();
			}
			
			return result;
		}
		
		@RosettaAttribute("a")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("a")
		@Override
		public RouteDeep.RouteDeepBuilder setA(RouteAltA _a) {
			this.a = _a == null ? null : _a.toBuilder();
			return this;
		}
		
		@RosettaAttribute("b")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("b")
		@Override
		public RouteDeep.RouteDeepBuilder setB(RouteAltB _b) {
			this.b = _b == null ? null : _b.toBuilder();
			return this;
		}
		
		@Override
		public RouteDeep build() {
			return new RouteDeep.RouteDeepImpl(this);
		}
		
		@Override
		public RouteDeep.RouteDeepBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public RouteDeep.RouteDeepBuilder prune() {
			if (a!=null && !a.prune().hasData()) a = null;
			if (b!=null && !b.prune().hasData()) b = null;
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getA()!=null && getA().hasData()) return true;
			if (getB()!=null && getB().hasData()) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public RouteDeep.RouteDeepBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			RouteDeep.RouteDeepBuilder o = (RouteDeep.RouteDeepBuilder) other;
			
			merger.mergeRosetta(getA(), o.getA(), this::setA);
			merger.mergeRosetta(getB(), o.getB(), this::setB);
			
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			RouteDeep _that = getType().cast(o);
		
			if (!Objects.equals(a, _that.getA())) return false;
			if (!Objects.equals(b, _that.getB())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (a != null ? a.hashCode() : 0);
			_result = 31 * _result + (b != null ? b.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "RouteDeepBuilder {" +
				"a=" + this.a + ", " +
				"b=" + this.b +
			'}';
		}
	}
}
