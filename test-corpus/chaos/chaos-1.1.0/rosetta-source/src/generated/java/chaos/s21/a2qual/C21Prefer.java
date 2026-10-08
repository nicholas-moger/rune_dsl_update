package chaos.s21.a2qual;

import chaos.s21.a2qual.h.C21Aux;
import chaos.s21.a2qual.meta.C21PreferMeta;
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

import static java.util.Optional.ofNullable;

/**
 * required and optional choice conditions side by side.
 * @version 1.0.0
 */
@RosettaDataType(value="C21Prefer", builder=C21Prefer.C21PreferBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C21Prefer", model="chaos", builder=C21Prefer.C21PreferBuilderImpl.class, version="1.0.0")
public interface C21Prefer extends RosettaModelObject {

	C21PreferMeta metaData = new C21PreferMeta();

	/*********************** Getter Methods  ***********************/
	String getX();
	String getY();
	String getZ();
	C21Aux getAux();

	/*********************** Build Methods  ***********************/
	C21Prefer build();
	
	C21Prefer.C21PreferBuilder toBuilder();
	
	static C21Prefer.C21PreferBuilder builder() {
		return new C21Prefer.C21PreferBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C21Prefer> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C21Prefer> getType() {
		return C21Prefer.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("x"), String.class, getX(), this);
		processor.processBasic(path.newSubPath("y"), String.class, getY(), this);
		processor.processBasic(path.newSubPath("z"), String.class, getZ(), this);
		processRosetta(path.newSubPath("aux"), processor, C21Aux.class, getAux());
	}
	

	/*********************** Builder Interface  ***********************/
	interface C21PreferBuilder extends C21Prefer, RosettaModelObjectBuilder {
		C21Aux.C21AuxBuilder getOrCreateAux();
		@Override
		C21Aux.C21AuxBuilder getAux();
		C21Prefer.C21PreferBuilder setX(String x);
		C21Prefer.C21PreferBuilder setY(String y);
		C21Prefer.C21PreferBuilder setZ(String z);
		C21Prefer.C21PreferBuilder setAux(C21Aux aux);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("x"), String.class, getX(), this);
			processor.processBasic(path.newSubPath("y"), String.class, getY(), this);
			processor.processBasic(path.newSubPath("z"), String.class, getZ(), this);
			processRosetta(path.newSubPath("aux"), processor, C21Aux.C21AuxBuilder.class, getAux());
		}
		

		C21Prefer.C21PreferBuilder prune();
	}

	/*********************** Immutable Implementation of C21Prefer  ***********************/
	class C21PreferImpl implements C21Prefer {
		private final String x;
		private final String y;
		private final String z;
		private final C21Aux aux;
		
		protected C21PreferImpl(C21Prefer.C21PreferBuilder builder) {
			this.x = builder.getX();
			this.y = builder.getY();
			this.z = builder.getZ();
			this.aux = ofNullable(builder.getAux()).map(f->f.build()).orElse(null);
		}
		
		@Override
		@RosettaAttribute("x")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("x")
		public String getX() {
			return x;
		}
		
		@Override
		@RosettaAttribute("y")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("y")
		public String getY() {
			return y;
		}
		
		@Override
		@RosettaAttribute("z")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("z")
		public String getZ() {
			return z;
		}
		
		@Override
		@RosettaAttribute("aux")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("aux")
		public C21Aux getAux() {
			return aux;
		}
		
		@Override
		public C21Prefer build() {
			return this;
		}
		
		@Override
		public C21Prefer.C21PreferBuilder toBuilder() {
			C21Prefer.C21PreferBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C21Prefer.C21PreferBuilder builder) {
			ofNullable(getX()).ifPresent(builder::setX);
			ofNullable(getY()).ifPresent(builder::setY);
			ofNullable(getZ()).ifPresent(builder::setZ);
			ofNullable(getAux()).ifPresent(builder::setAux);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C21Prefer _that = getType().cast(o);
		
			if (!Objects.equals(x, _that.getX())) return false;
			if (!Objects.equals(y, _that.getY())) return false;
			if (!Objects.equals(z, _that.getZ())) return false;
			if (!Objects.equals(aux, _that.getAux())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (x != null ? x.hashCode() : 0);
			_result = 31 * _result + (y != null ? y.hashCode() : 0);
			_result = 31 * _result + (z != null ? z.hashCode() : 0);
			_result = 31 * _result + (aux != null ? aux.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C21Prefer {" +
				"x=" + this.x + ", " +
				"y=" + this.y + ", " +
				"z=" + this.z + ", " +
				"aux=" + this.aux +
			'}';
		}
	}

	/*********************** Builder Implementation of C21Prefer  ***********************/
	class C21PreferBuilderImpl implements C21Prefer.C21PreferBuilder {
	
		protected String x;
		protected String y;
		protected String z;
		protected C21Aux.C21AuxBuilder aux;
		
		@Override
		@RosettaAttribute("x")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("x")
		public String getX() {
			return x;
		}
		
		@Override
		@RosettaAttribute("y")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("y")
		public String getY() {
			return y;
		}
		
		@Override
		@RosettaAttribute("z")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("z")
		public String getZ() {
			return z;
		}
		
		@Override
		@RosettaAttribute("aux")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("aux")
		public C21Aux.C21AuxBuilder getAux() {
			return aux;
		}
		
		@Override
		public C21Aux.C21AuxBuilder getOrCreateAux() {
			C21Aux.C21AuxBuilder result;
			if (aux!=null) {
				result = aux;
			}
			else {
				result = aux = C21Aux.builder();
			}
			
			return result;
		}
		
		@RosettaAttribute("x")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("x")
		@Override
		public C21Prefer.C21PreferBuilder setX(String _x) {
			this.x = _x == null ? null : _x;
			return this;
		}
		
		@RosettaAttribute("y")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("y")
		@Override
		public C21Prefer.C21PreferBuilder setY(String _y) {
			this.y = _y == null ? null : _y;
			return this;
		}
		
		@RosettaAttribute("z")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("z")
		@Override
		public C21Prefer.C21PreferBuilder setZ(String _z) {
			this.z = _z == null ? null : _z;
			return this;
		}
		
		@RosettaAttribute("aux")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("aux")
		@Override
		public C21Prefer.C21PreferBuilder setAux(C21Aux _aux) {
			this.aux = _aux == null ? null : _aux.toBuilder();
			return this;
		}
		
		@Override
		public C21Prefer build() {
			return new C21Prefer.C21PreferImpl(this);
		}
		
		@Override
		public C21Prefer.C21PreferBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C21Prefer.C21PreferBuilder prune() {
			if (aux!=null && !aux.prune().hasData()) aux = null;
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getX()!=null) return true;
			if (getY()!=null) return true;
			if (getZ()!=null) return true;
			if (getAux()!=null && getAux().hasData()) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C21Prefer.C21PreferBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C21Prefer.C21PreferBuilder o = (C21Prefer.C21PreferBuilder) other;
			
			merger.mergeRosetta(getAux(), o.getAux(), this::setAux);
			
			merger.mergeBasic(getX(), o.getX(), this::setX);
			merger.mergeBasic(getY(), o.getY(), this::setY);
			merger.mergeBasic(getZ(), o.getZ(), this::setZ);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C21Prefer _that = getType().cast(o);
		
			if (!Objects.equals(x, _that.getX())) return false;
			if (!Objects.equals(y, _that.getY())) return false;
			if (!Objects.equals(z, _that.getZ())) return false;
			if (!Objects.equals(aux, _that.getAux())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (x != null ? x.hashCode() : 0);
			_result = 31 * _result + (y != null ? y.hashCode() : 0);
			_result = 31 * _result + (z != null ? z.hashCode() : 0);
			_result = 31 * _result + (aux != null ? aux.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C21PreferBuilder {" +
				"x=" + this.x + ", " +
				"y=" + this.y + ", " +
				"z=" + this.z + ", " +
				"aux=" + this.aux +
			'}';
		}
	}
}
